package jp.ac.u_tokyo.t.seo.station.diagram;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.*;

/**
 * ドロネー図における母点を結ぶ線分の垂直二等分線は互いに交差し、その交点によって二等分線は分割される.
 * その交点をNode、交点によって分割されてできた線分を辺Edgeにもつグラフ構造を考える
 * @author Seo-4d696b75
 * @version 2019/06/12
 */
public class HighVoronoi<E extends Point> {

    /**
     * 高次ボロノイ図を解決するうえで必要となる頂点を提供する. <br>
     * ボロノイ図の双対図であるドロネー図が解決済みであると仮定して以下のように頂点の隣接関係を定義する.<br>
     * <strong> 隣接する点 = ドロネー図の各辺における両端点 </strong>
     */
    public interface PointProvider<T extends Point> {

        /**
         * 指定された点に隣接する頂点集合を返す.
         * 条件を満たす要素がない場合は空のリストを返す.<br>
         *
         * @param point Non Null
         * @return Non Null
         */
        Collection<T> getNeighbors(T point);

    }

    public interface ResultCallback {
        /**
         * 各n次ボロノイ図が計算されると順次呼ばれる
         * @param index 次数[1,({@link HighVoronoi#solve(int, Point, PointProvider, ResultCallback)}  指定した値})]
         * @param points 閉じた多角形
         */
        void onResolved(int index, Polygon points, long time);
        
        void onCompleted(Polygon[] results, long time);
    }

    public HighVoronoi(Triangle frame) {
        container = frame;
    }

    private E center;
    private List<Bisector> bisectors;
    private Triangle container;
    private PointProvider<E> resolver;
    private Set<Point> requestedPoint;
    private Set<Point> addedPoint;
    private Queue<E> requestQueue;
    private boolean extensionRunning, traverseRunning;

    public Polygon[] solve(int level, E center, PointProvider<E> resolver, ResultCallback callback) {
        this.center = center;
        this.resolver = resolver;

        Setting.error = Math.pow(10, -10);

        long time = System.currentTimeMillis();

        Polygon[] result = new Polygon[level];
        bisectors = new LinkedList<>();

        addBoundary(new Line(container.a, container.b));
        addBoundary(new Line(container.b, container.c));
        addBoundary(new Line(container.c, container.a));

        requestedPoint = new HashSet<>();
        addedPoint = new HashSet<>();
        requestQueue = new LinkedList<>();

        requestedPoint.add(center);
        addedPoint.add(center);
        for ( E point : resolver.getNeighbors(center) ){
            addedPoint.add(point);
            addBisector(point);
        }

        List<Node> list = null;

        for ( int targetLevel = 1 ; targetLevel <= level ; targetLevel++ ){
            long loopTime = System.currentTimeMillis();

            startThread();

            list = traverse(list);
            for ( Node n : list ) n.onSolved(targetLevel);

            Polygon polygon = new Polygon(list);
            result[targetLevel - 1] = polygon;

            joinThread();

            if (callback != null) {
                callback.onResolved(targetLevel - 1, polygon, System.currentTimeMillis() - loopTime);
            }

            System.out.println("solve > " + String.format(Locale.US, "index:%d, time:%dms", targetLevel, System.currentTimeMillis() - loopTime));
        }


        for ( Bisector bisector : bisectors ){
            bisector.release();
        }

        if ( callback != null ){
            callback.onCompleted(result, System.currentTimeMillis() - time);
        }
        //System.out.println("HighVoronoi#solve > " + String.format(Locale.US, "time:%dms vertex:%d", System.currentTimeMillis() - time, bisectors.size()));

        return result;

    }

    private List<Node> traverse(List<Node> list){
        Node next = null;
        Point previous = null;
        if ( list == null ){
            Set<Point> history = new HashSet<>();
            Bisector sample = bisectors.get(0);
            next = sample.intersections.get(1).node;
            previous = sample.intersections.get(0);
            while ( history.add(next) ){
                Node current = next;
                next = current.nextDown(previous);
                previous = current;
            }
        }else{
            previous = list.get(list.size()-1);
            for ( Node n : list ){
                next = n.nextUp(previous);
                previous = n;
                if ( next != null && !next.hasSolved() ) break;
            }
        }

        if ( next == null || previous == null || next.hasSolved() ){
            throw new RuntimeException("piyo");
        }

        Node start = next;
        list = new LinkedList<>();
        list.add(start);
        while ( true ) {
            requestExtension(next.p1.line.delaunayPoint);
            requestExtension(next.p2.line.delaunayPoint);
            Node current = next;
            next = current.next(previous);
            previous = current;
            if ( start.equals(next) ) break;
            list.add(next);
        }
        return list;
    }

    private void requestExtension(E point) {
        if ( point != null && requestedPoint.add(point)) {
            synchronized (this) {
                // ExtensionTask が待ちで寝ていたら起こす
                if (requestQueue.isEmpty()) notifyAll();
                for ( E p : resolver.getNeighbors(point) ){
                    if ( addedPoint.add(p) ){
                        requestQueue.offer(p);
                    }
                }
            }
        }
    }

    private synchronized E dequeueRequest() {
        while (traverseRunning || !requestQueue.isEmpty()) {
            if (requestQueue.isEmpty()) {
                // メインスレッドの走査が継続中かつ待ち行列が空なら待つ
                // Not busy wait!!
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                return requestQueue.remove();
            }
        }
        return null;
    }

    private synchronized void onExtensionComplete(int cnt, long elapsedTime) {
        extensionRunning = false;
        //System.out.println(String.format("addBisector > size:%d time:%d", cnt, elapsedTime));
        // 走査を既に終えたメインスレッドが待っている場合もあるので起こしてみる
        notifyAll();
    }

    private void startThread() {
        traverseRunning = true;
        extensionRunning = true;
        new Thread(new Runnable() {

            private long elapsedTime = 0;
            private int cnt;

            @Override
            public void run() {
                while (true) {
                    E request = dequeueRequest();
                    if (request == null) break;
                    cnt++;
                    long time = System.currentTimeMillis();
                    addBisector(request);
                    elapsedTime += (System.currentTimeMillis() - time);
                }
                onExtensionComplete(cnt, elapsedTime);
            }

        }).start();
    }

    private synchronized void joinThread() {
        traverseRunning = false;
        // ExtensionTaskが仕事が無く寝ているかもしれない
        if (requestQueue.isEmpty()) notifyAll();
        while (extensionRunning) {
            // ExtensionTaskを待つ
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void addBoundary(Line self){
        Bisector boundary = new Bisector(self);
        for ( Bisector preexist : bisectors ){
            Point p = boundary.line.getIntersection(preexist.line);
            Intersection a = new Intersection(p, boundary);
            Intersection b = new Intersection(p, preexist);
            Node n = new Node(p, a, b);
            a.node = n;
            b.node = n;
            boundary.addIntersection(a);
            preexist.addIntersection(b);
        }
        bisectors.add(boundary);
    }

    private void addBisector(E point) {
        Bisector bisector = new Bisector(point, Line.getPerpendicularBisector(point, center));
        for (Bisector preexist : bisectors) {
            Point p = bisector.line.getIntersection(preexist.line);
            if ( p != null && container.containsPoint(p) ) {
                Intersection a = new Intersection(p, bisector, preexist.line);
                Intersection b = new Intersection(p, preexist, bisector.line);
                Node n = new Node(p, a, b);
                a.node = n;
                b.node = n;

                bisector.addIntersection(a);
                preexist.addIntersection(b);
            }
        }
        bisectors.add(bisector);
    }

    private class Node extends Point {

        static final int STEP_UP = 1;
        static final int STEP_DOWN = -1;
        static final int STEP_ZERO = 0;

        private Node(Point point, Intersection a, Intersection b){
            this.point = point;
            p1 = a;
            p2 = b;
            int cnt = 0;
            if ( a.line.isBoundary ) cnt++;
            if ( b.line.isBoundary ) cnt++;
            if ( cnt == 0 ){
                onBoundary = false;
                index = -1;
            }else if ( cnt == 1 ){
                onBoundary = true;
                index = -1;
            }else{
                onBoundary = false;
            }
        }

        final boolean onBoundary;
        final Point point;
        private Intersection p1, p2;
        private float index;

        @Override
        public double getX() {
            return point.getX();
        }

        @Override
        public double getY() {
            return point.getY();
        }

        /**
         * 辿ってきた辺とは異なる線分上の隣接頂点でかつ辺のボロノイ次数が同じになる方を返す.
         * @param previous from which you are traversing
         * @return Voronoi-Index of Edge:previous=>this is same as that of Edge:this=>next
         */
        Node next(Point previous){
            if ( p1.hasNext() && p1.next().equals(previous) ){
                return next(p1, p2, false, -p1.getStep());
            }else if ( p1.hasPrevious() && p1.previous().equals(previous) ){
                return next(p1, p2, true, p1.getStep());
            }else if ( p2.hasNext() && p2.next().equals(previous) ){
                return next(p2, p1, false, -p2.getStep());
            }else if ( p2.hasPrevious() && p2.previous().equals(previous) ){
                return next(p2, p1, true, p2.getStep());
            }else{
                throw new NoSuchElementException("not found");
            }
        }

        private Node next(Intersection current, Intersection other, boolean forward, int step){
            if ( onBoundary && index > 0 ){
                // 頂点がFrame境界線上（Vertexではない）でかつ
                // この頂点が解決済みなら無視して同じ境界線上のお隣さんへ辿る
                return forward ? current.next().node : current.previous().node;
            }else{
                // 頂点がFrame内部なら step = Node.STEP_UP/DOWN　のいずれか
                // FrameのVertexに位置する場合は例外的に step = Node.STEP_ZERO
                return other.neighbor(-step).node;
            }
        }

        /**
         * 辿ってきた辺とは異なる線分上の隣接頂点のうちこの頂点から見てボロノイ次数が
         * 下がるまたは変化しない方を返す.<br>
         * この頂点がFrame内部なら必ず次数が下がる隣接頂点を返すが、
         * Frame境界線のVertexに相当する場合は例外的に次数変化0の方向の頂点を返す
         * @param previous from which you are traversing
         * @return Not null
         */
        @NonNull
        Node nextDown(Point previous){
            Intersection target = null;
            if ( p1.isNeighbor(previous) ){
                target = p2;
                //return p2.neighbor(Node.STEP_DOWN).node;
            }else if ( p2.isNeighbor(previous) ){
                target = p1;
                //return p1.neighbor(Node.STEP_DOWN).node;
            }
            if ( target == null ) {
                throw new NoSuchElementException("not found");
            }
            if ( target.hasNeighbor(Node.STEP_DOWN) ){
                return target.neighbor(Node.STEP_DOWN).node;
            }else{
                return target.neighbor(Node.STEP_ZERO).node;
            }
        }

        /**
         * この頂点から見てボロノイ次数が上がる方向の隣接頂点を返す<br>
         * 辿ってきた方向{@code previous => this}に対して異なる線分上、同じ線分上の順で探す。
         * それでも存在しない場合はNull
         * @param previous　from which you are traversing
         * @return Null if no such node
         */
        @Nullable
        Node nextUp(Point previous){
            Intersection t1 = null;
            Intersection t2 = null;
            if ( p1.isNeighbor(previous) ){
                t1 = p2;
                t2 = p1;
                //return p2.neighbor(Node.STEP_UP).node;
            }else if ( p2.isNeighbor(previous) ){
                t1 = p1;
                t2 = p2;
                //return p1.neighbor(Node.STEP_UP).node;
            }
            if ( t1 == null || t2 == null ) {
                throw new NoSuchElementException("not found");
            }
            if ( t1.hasNeighbor(Node.STEP_UP) ){
                return t1.neighbor(Node.STEP_UP).node;
            }else if ( t2.hasNeighbor(Node.STEP_UP) ){
                return t2.neighbor(Node.STEP_UP).node;
            }
            return null;
        }

        void onSolved(int level){
            p1.onSolved();
            p2.onSolved();
            if ( index < 0 ){
                if ( p1.line.isBoundary || p2.line.isBoundary ){
                    // フレーム上の頂点はn次以上のボロノイ図に登場する
                    index = level;
                }else {
                    // フレーム内部の頂点はn,n+1次のボロノイ図にしか登場しない
                    index = level + 0.5f;
                }
            }else if ( Math.round(index) != index ){
                // 整合性の確認
                if ( index + 0.5f != level ) throw new RuntimeException("index mismatch");
            }
        }

        boolean hasSolved(){
            return index >= 0f;
        }

        void release(){
            p1 = null;
            p2 = null;
        }

        @Override
        public String toString(){
            if ( index < 0 ){
                return String.format("%s not solved", super.toString());
            }else if ( index == 0 ){
                return String.format("%s Vertex", super.toString());
            }else{
                return String.format("%s %.1f", super.toString(), index);
            }
        }

    }

    private class Intersection extends Point{

        private Intersection(Point point, Bisector line, Line other){
            this.point = point;
            this.line = line;

            double dx = line.line.b;
            double dy = -line.line.a;
            if (dx < 0 || (dx == 0 && dy < 0)) {
                dx *= -1;
                dy *= -1;
            }
            Point p = new BasePoint(point.getX() + dx, point.getY() + dy);
            this.step = other.onSameSide(p, center) ? Node.STEP_DOWN : Node.STEP_UP;
        }

        private Intersection(Point point, Bisector line){
            this.point = point;
            this.line = line;
            this.step = Node.STEP_ZERO;
        }

        final Point point;
        final private Bisector line;
        final int step;
        private Node node;

        private int index = -1;
        private Intersection previous, next;

        @Override
        public double getX() {
            return point.getX();
        }

        @Override
        public double getY() {
            return point.getY();
        }


        int getStep(){
            return step;
        }

        void insert(Intersection previous, Intersection next, int index){
            this.previous = previous;
            this.next = next;
            if ( previous != null ) previous.next = this;
            if ( next != null ){
                next.previous = this;
                next.incrementIndex();
            }
            this.index = index;
        }

        void incrementIndex(){
            index++;
            if ( next != null ) next.incrementIndex();
        }

        boolean hasPrevious(){
            return previous != null;
        }

        boolean hasNext(){
            return next != null;
        }

        boolean isNeighbor(Point p){
            return ( hasNext() && next.equals(p) ) || ( hasPrevious() && previous.equals(p));
        }

        boolean hasNeighbor(int step){
            if ( step == 0 && this.step == 0 ){
                return true;
            }else if ( step != 0 && this.step != 0 ){
                return ( step == this.step ) ? hasNext() : hasPrevious();
            }
            return false;
        }

        Intersection neighbor(int step){
            if ( step == 0 && this.step == 0 ){
                if ( hasPrevious() ) return previous;
                if ( hasNext() ) return next;
            }else if ( step != 0 && this.step != 0 ){
                return ( step == this.step ) ? next() : previous();
            }
            throw new IllegalArgumentException("step invalid");
        }

        Intersection next(){
            if ( next == null ){
                throw new NoSuchElementException();
            }
            return next;
        }

        Intersection previous(){
            if ( previous == null ){
                throw new NoSuchElementException();
            }
            return previous;
        }

        int getIndex(){
            if ( index < 0 ) throw new IllegalStateException("index not set yet");
            return index;
        }

        void setIndex(int index){
            this.index = index;
        }

        void onSolved(){
            line.onIntersectionSolved(this);
        }

        void release(){
            previous = null;
            next = null;
            if ( node == null ){
                return;
            }
            node.release();
            node = null;
        }

        @Override
        public String toString(){
            return String.format("%s %+d", super.toString(), step);
        }

    }

    private class Bisector {

        private Bisector(E point, Line line){
            this.delaunayPoint = point;
            this.line = line;
            intersections = new LinkedList<>();
            isBoundary = false;

            /*inspectBoundary(boundaryA);
            inspectBoundary(boundaryB);
            inspectBoundary(boundaryC);
            if ( intersections.size() != 2 ){
                throw new RuntimeException("hoge");
            }*/
        }

        // special for boundary line
        private Bisector(Line edge){
            delaunayPoint = null;
            line = edge;
            isBoundary = true;
            intersections = new LinkedList<>();
            //addIntersection(new Intersection(edge.a, this));
            //addIntersection(new Intersection(edge.b, this));
        }

        private void inspectBoundary(Edge boundary){
            Point p = boundary.getIntersection(line);
            if ( p != null ){
                Intersection i = new Intersection(p, this, boundary.toLine());
                addIntersection(i);
            }
        }

        private List<Intersection> intersections;
        private int solvedPointIndexFrom = Integer.MAX_VALUE;
        private int solvedPointIndexTo = -1;

        final E delaunayPoint;
        final Line line;
        final boolean isBoundary;

        synchronized void onIntersectionSolved(Intersection intersection){
            int index = intersection.getIndex();
            solvedPointIndexFrom = Math.min(solvedPointIndexFrom, index);
            solvedPointIndexTo = Math.max(solvedPointIndexTo, index);
            //requestExtension(delaunayPoint);
        }

        synchronized void addIntersection(Intersection intersection) {
            final int size = intersections.size();
            int index = addIntersection(intersection, 0, size);

            intersection.insert(
                    index > 0 ? intersections.get(index-1) : null,
                    index < size ? intersections.get(index) : null,
                    index
            );
            intersections.add(index, intersection);
            if (solvedPointIndexFrom < solvedPointIndexTo) {
                if (index <= solvedPointIndexFrom) {
                    solvedPointIndexFrom++;
                    solvedPointIndexTo++;
                } else {
                    if (index <= solvedPointIndexTo) {
                        throw new IllegalStateException("new intersection added to resolved-range!!");
                    }
                }
            }
        }

        private int addIntersection(Intersection point, int indexFrom, int indexTo) {
            if (indexFrom == indexTo) {
                return indexFrom;
            } else {
                int mid = (indexFrom + indexTo - 1) / 2;
                // TODO if result == 0, yet may not be
                int result = Point.compare(point, intersections.get(mid));
                if (result < 0) {
                    return addIntersection(point, indexFrom, mid);
                } else {
                    return addIntersection(point, mid + 1, indexTo);
                }
            }
        }

        void release(){
            for ( Intersection item : intersections ) item.release();
            intersections.clear();
            intersections = null;
        }

    }

}

