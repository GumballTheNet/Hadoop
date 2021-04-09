import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

public class QueryHost implements WritableComparable<QueryHost> {
    private Text query;
    private Text host;
    
    private void set(Text query, Text host) {
        this.query = query;
        this.host = host;
    }

    public void set(QueryHost other) {
        this.query.set(other.query);
        this.host.set(other.host);
    }

    public QueryHost() {
        set(new Text(), new Text());
    }
	
    public QueryHost(String query, String host) {
        set(new Text(query), new Text(host));
    }

    public QueryHost(Text query, Text host) {
        set(query, host);
    }

    public Text getQuery() {
        return query;
    }

    public Text getHost() {
        return host;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        query.write(out);
        host.write(out);
    }
    
    @Override
    public String toString() {
        return host + "\t" + query;
    }
    
    @Override
    public int compareTo(QueryHost o) {
        int cmp = host.compareTo(o.host);
        return (cmp == 0) ? -query.compareTo(o.query) : cmp;
    }
    
    public static class KeyComparator extends WritableComparator {
        protected KeyComparator() {
            super(QueryHost.class, true);
        }

        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return ((QueryHost)a).compareTo((QueryHost)b);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
         QueryHost tp = (QueryHost) obj;
         return host.equals(tp.host) && query.equals(tp.query);
  
    }
    
    @Override
    public void readFields(DataInput dataInput) throws IOException {
        query.readFields(dataInput);
        host.readFields(dataInput);
    }

    @Override
    public int hashCode() {
        return query.hashCode() * 256 + host.hashCode();
    } 
}
