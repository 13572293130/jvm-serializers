package serializers.jackson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import data.media.MediaContent;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;

import serializers.JavaBuiltIn;
import serializers.Serializer;
import serializers.TestGroups;
import de.undercouch.bson4jackson.BsonFactory;

/**
 * This serializer uses bson4jackson in full automated data binding mode, which
 * can handle typical Java POJOs (esp. beans; otherwise may need to annotate
 * to configure)
 */
public class JacksonBsonDatabind
{
    public static void register(TestGroups groups)
    {
        ObjectMapper mapper = new ObjectMapper(new BsonFactory());
        groups.media.add(JavaBuiltIn.mediaTransformer,
                new DataBindBase<MediaContent>(
                        "bson/jackson/databind", MediaContent.class, mapper));
    }

    // Must bundle, because BSON module still uses Jackson 1.x...
    
    public final static class DataBindBase<T> extends Serializer<T>
    {
        protected final String name;
        protected final JavaType type;
        protected final ObjectMapper mapper;
        
        public DataBindBase(String name, Class<T> clazz, ObjectMapper mapper)
        {
            this.name = name;
            type = mapper.getTypeFactory().constructType(clazz);
            this.mapper = mapper;
        }

        @Override
        public final String getName() {
            return name;
        }

        protected final JsonParser constructParser(byte[] data) throws IOException {
            return mapper.getJsonFactory().createJsonParser(data, 0, data.length);
        }

        protected final JsonParser constructParser(InputStream in) throws IOException {
            return mapper.getJsonFactory().createJsonParser(in);
        }
        
        protected final JsonGenerator constructGenerator(OutputStream out) throws IOException {
            return mapper.getJsonFactory().createJsonGenerator(out, JsonEncoding.UTF8);
        }
        
        @Override
        public byte[] serialize(T data) throws IOException
        {
            return mapper.writeValueAsBytes(data);
        }
    
        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(byte[] array) throws IOException
        {
            return (T) mapper.readValue(array, 0, array.length, type);
        }
    
        // // Future extensions for testing performance for item sequences
        
        @Override
        public void serializeItems(T[] items, OutputStream out) throws IOException
        {
            JsonGenerator generator = constructGenerator(out);
            // JSON allows simple sequences, so:
            for (int i = 0, len = items.length; i < len; ++i) {
                mapper.writeValue(generator, items[i]);
            }
            generator.close();
        }
    
        @Override
        @SuppressWarnings("unchecked")
        public T[] deserializeItems(InputStream in, int numberOfItems) throws IOException 
        {
            T[] result = (T[]) new Object[numberOfItems];
            JsonParser parser = constructParser(in);
            for (int i = 0; i < numberOfItems; ++i) {
                result[i] = (T) mapper.readValue(parser, type);
            }
            parser.close();
            return result;
        }
    }
}
