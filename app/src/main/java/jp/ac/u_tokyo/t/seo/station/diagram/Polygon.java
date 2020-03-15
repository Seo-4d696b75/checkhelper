package jp.ac.u_tokyo.t.seo.station.diagram;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author Seo-4d696b75
 * @version 2018/05/17
 */
public class Polygon implements Iterable<Point> {

    public static class Builder{

        private List<EdgeGroup> groups;
        private boolean closed = false;

        private Set<Edge> edgeSet;
        private Set<Point> solvedPointSet;

        private class EdgeGroup{
            EdgeGroup(Edge edge){
                list = new LinkedList<>();
                list.add(edge.a);
                list.add(edge.b);
                p1 = edge.a;
                p2 = edge.b;
            }
            List<Point> list;
            Point p1,p2;
            boolean closed(){
                return Point.isMatch(p1, p2);
            }
            boolean merge(EdgeGroup group){
                if ( p1.equals(group.p1) ){
                    solvedPointSet.add(p1);
                    p1 = group.p2;
                    group.list.remove(0);
                    for ( Point point : group.list ){
                        this.list.add(0, point);
                    }
                }else if ( p1.equals(group.p2) ){
                    solvedPointSet.add(p1);
                    p1 = group.p1;
                    this.list.remove(0);
                    group.list.addAll(this.list);
                    this.list = group.list;
                }else if ( p2.equals(group.p1) ){
                    solvedPointSet.add(p2);
                    p2 = group.p2;
                    group.list.remove(0);
                    this.list.addAll(group.list);
                }else if ( p2.equals(group.p2) ){
                    solvedPointSet.add(p2);
                    p2 = group.p1;
                    for ( int i=group.list.size()-2 ; i>=0 ; i-- ){
                        this.list.add(group.list.get(i));
                    }
                }else{
                    return false;
                }
                return true;
            }
            @Override
            public String toString(){
                StringBuilder builder = new StringBuilder();
                builder.append("Builder#Group{point_list:[\n");
                for ( Point p : list ){
                    builder.append(p.toString());
                    builder.append('\n');
                }
                builder.append("]}");
                return builder.toString();
            }
        }

        public Builder(){
            groups = new ArrayList<>();
            edgeSet = new HashSet<>();
            solvedPointSet = new HashSet<>();
        }


        public void append(Edge edge){
            if ( closed ){
                throw new IllegalStateException("Polygon already closed.");
            }
            if ( !edgeSet.add(edge) ) return;
            if ( solvedPointSet.contains(edge.a) || solvedPointSet.contains(edge.b) ){
                throw new IllegalArgumentException("Point already appended and connected. " + edge.toString());
            }
            EdgeGroup group = new EdgeGroup(edge);

            for (Iterator<EdgeGroup> iterator = groups.iterator() ; iterator.hasNext() ; ){
                EdgeGroup next = iterator.next();
                if ( group.merge(next) ){
                    iterator.remove();
                }
            }
            groups.add(group);
            closed = groups.size() == 1 && group.closed();
        }

        public boolean isClosed(){
            return closed;
        }

        public boolean isLine(){
            return groups.size() == 1;
        }

        public List<Point> getLine(){
            if ( isLine() ){
                return groups.get(0).list;
            }else{
                return null;
            }
        }

        public Polygon build(){
            if ( closed ){
                EdgeGroup group = groups.get(0);
                group.list.remove(group.list.size()-1);
                // 自己交錯は考慮しない
                /*
                final int size = group.list.size();
                if ( size < 3 ){
                    return null;
                }
                Point p1 = group.list.get(size-2);
                Point p2 = group.list.get(size-1);
                double sum = 0.0;
                for ( Point p3 : group.list ){
                    double ax = p2.getX() - p1.getX();
                    double ay = p2.getY() - p1.getY();
                    double bx = p3.getX() - p2.getX();
                    double by = p3.getY() - p2.getY();
                    double cross = ax*by - ay*bx;
                    double argument = Math.atan( cross / (ax*bx + ay*by));
                    if ( argument < 0 ) argument += Math.PI;
                    sum = cross > 0 ? sum + argument : sum - argument;
                    p1 = p2;
                    p2 = p3;
                }*/
                //if ( Math.abs(Math.PI*2 - Math.abs(sum)) < Setting.error ){
                    return new Polygon(group.list);
                //}
            }
            return null;
        }



    }

    public Polygon(List<? extends Point> points){
        this.points = new ArrayList<>(points.size());
        this.points.addAll(points);
    }

    private List<Point> points;

    public int size(){
        return points.size();
    }

    @Override
    public Iterator<Point> iterator() {
        return points.iterator();
    }

    public List<Point> getPoints(){
        return points;
    }

    public List<Edge> getEdges(){
        List<Edge> list = new ArrayList<>(size());
        Point previous = points.get(size()-1);
        for ( Point next : points ){
            list.add(new Edge(previous, next));
            previous = next;
        }
        return list;
    }

    public Rectangle getRectBound(){
        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double bottom = Double.MAX_VALUE;
        double top = -Double.MAX_VALUE;
        for ( Point p : this ){
            if ( p.getX() < left ) left = p.getX();
            if ( p.getX() > right ) right = p.getX();
            if ( p.getY() < bottom ) bottom = p.getY();
            if ( p.getY() > top ) top = p.getY();
        }
        return new Rectangle(left, top, right, bottom);
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("Polygon{size:");
        builder.append(points.size());
        builder.append(",points:[\n");
        for ( Point p : points ){
            builder.append(p.toString());
            builder.append('\n');
        }
        builder.append("]}");
        return builder.toString();
    }

    public static class LoopIterator<E> implements ListIterator<E>{

        LoopIterator(List<E> list, int startIndex, boolean forward){
            if ( startIndex < 0 || startIndex >= list.size() ){
                throw new IndexOutOfBoundsException();
            }
            mSize = list.size();
            mList = list;
            mCurrentIndex = startIndex;
            mDirection = forward ? 1 : -1;
            reset();
        }

        LoopIterator(List<E> list, int startIndex){
            this(list, startIndex, true);
        }

        LoopIterator(List<E> list){
            this(list, 0, true);
        }

        private final List<E> mList;
        private int mSize;
        private int mDirection;
        private int mStartIndex;
        private int mCurrentIndex;
        private int mCanRemove;

        public E peekNext(){
            return hasNext() ? mList.get(mCurrentIndex%mSize) : null;
        }

        public E peekPrevious(){
            return hasPrevious() ? mList.get((mCurrentIndex - mDirection) % mSize) : null;
        }

        @Override
        public E next(){
            if ( hasNext() ) {
                mCanRemove = 1;
                E next = mList.get(mCurrentIndex % mSize);
                mCurrentIndex += mDirection;
                return next;
            }
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return previousIndex() > mStartIndex - mSize && previousIndex() < mStartIndex + mSize;
        }

        @Override
        public E previous() {
            if ( hasPrevious() ){
                mCanRemove = 0;
                mCurrentIndex -= mDirection;
                return mList.get(mCurrentIndex % mSize);
            }
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return mCurrentIndex;
        }

        @Override
        public int previousIndex() {
            return mCurrentIndex - mDirection;
        }

        @Override
        public void remove() {
            if ( mCanRemove < 0 ){
                throw new IllegalStateException();
            }
            final int targetIndex = mCurrentIndex - mDirection*mCanRemove;
            mList.remove(targetIndex % mSize);
            mCanRemove = -1;
            if ( targetIndex%mSize < mStartIndex%mSize ) mStartIndex--;
            mSize--;
            mCurrentIndex = mDirection > 0 ? targetIndex - 1 : targetIndex - 2;
            mStartIndex--;
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext(){
            return nextIndex() > mStartIndex - mSize && nextIndex() < mStartIndex + mSize;
        }

        public void changeDirection(){
            setDirection(mDirection < 0);
        }

        public void setDirection(boolean forward){
            if ( forward == mDirection > 0 ) return;
            mDirection = -mDirection;
            mCurrentIndex = mCurrentIndex + mDirection;
            reset();
        }

        public void reset(){
            mStartIndex = mCurrentIndex%mSize + mSize;
            mCurrentIndex = mStartIndex;
            mCanRemove = -1;
        }

        @Override
        public void add(E element){
            mCanRemove = -1;
            final int targetIndex = mDirection > 0 ? mCurrentIndex : mCurrentIndex + 1;
            mList.add(targetIndex%mSize, element);
            if ( targetIndex%mSize <= mStartIndex%mSize ) mStartIndex++;
            mSize++;
            mCurrentIndex = mDirection > 0 ? targetIndex + 2 : targetIndex;
            mStartIndex++;
        }

    }

    public static class MergeBuilder{

        public MergeBuilder(){
            mList = new LinkedList<>();
        }

        public void append(Polygon polygon){
            Entry entry = new Entry(polygon);
            for ( ListIterator<Entry> iterator = mList.listIterator() ; iterator.hasNext() ; ){
                Entry item = iterator.next();
                if ( entry.merge(item) ) iterator.remove();
            }
            mList.add(entry);
        }

        public List<Polygon> build(){
            List<Polygon> polygons = new ArrayList<>(mList.size());
            for ( Entry item : mList ){
                if ( item.mHasHollow ){
                    return null;
                }
                polygons.add(new Polygon(item.mPointList));
            }
            return polygons;
        }

        private final List<Entry> mList;

        private static class Entry{

            Entry(Polygon polygon){
                mPointList = new LinkedList<>();
                mPointSet = new HashSet<>();
                mHasHollow = false;
            }

            final List<Point> mPointList;
            final Set<Point> mPointSet;
            boolean mHasHollow;


            boolean merge(Entry entry){
                final List<Point> list = entry.mPointList;
                final int size = list.size();
                for ( int i=0 ; i<size ; i++ ){
                    if ( mPointSet.contains(list.get(i)) ){
                        int j = 0;
                        for ( ; j<mPointList.size() ; j++ ){
                            if ( mPointList.get(j).equals(list.get(i)) ) break;
                        }
                        LoopIterator<Point> self = new LoopIterator<Point>(mPointList, j);
                        LoopIterator<Point> other = new LoopIterator<Point>(list, i);
                        other.next();
                        Point next = other.peekNext();
                        other.previous();
                        Point previous = self.peekPrevious();
                        self.next();
                        if ( self.peekNext().equals(next) ){
                            self.previous();
                            merge(self, other);
                        }else if ( previous.equals(next) ){
                            self.setDirection(false);
                            merge(self, other);
                        }else{
                            return false;
                        }
                        return true;
                    }
                }
                return false;
            }

            private void merge(LoopIterator<Point> self, LoopIterator<Point> other){
                while ( true ){
                    Point nextSelf = self.peekNext();
                    Point nextOther = other.peekNext();
                    if ( !nextOther.equals(nextSelf) ) break;
                    self.next();
                    self.remove();
                    other.next();
                    mPointSet.remove(nextSelf);
                }
                self.changeDirection();
                final boolean hasHollow = mHasHollow;
                while( other.hasNext() ){
                    Point next = other.next();
                    if ( !mPointSet.add(next) ) mHasHollow = true;
                    self.add(next);
                }
                if ( hasHollow ) mHasHollow = checkHollow();
            }

            private boolean checkHollow(){
                Point previous = mPointList.get(mPointList.size()-1);
                final int size = mPointList.size();
                for ( int i=0 ; i<size ; i++ ){
                    Point next  = mPointList.get(i);
                    if ( next.equals(previous) ){
                        int start = size + i -1;
                        int end = size + i;
                        while ( mPointList.get(start%size).equals(mPointList.get(end%size)) ){
                            start--;
                            end++;
                        }
                        LoopIterator<Point> iterator = new LoopIterator<Point>(mPointList, start%size);
                        for ( i=0 ; i<= end-start ; i++ ){
                            iterator.next();
                            iterator.remove();
                        }
                        mPointSet.clear();
                        boolean hollow = false;
                        for ( Point point : mPointList ){
                            if ( !mPointSet.add(point) ) hollow = true;
                        }
                        return hollow;
                    }
                    previous = next;
                }
                return mHasHollow;
            }

        }

    }

}
