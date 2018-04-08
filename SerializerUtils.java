
public class SerializerUtils {
    @Test
    public void test_() {
        List<Person> folks = Arrays.asList(new Person("Joe"), new Person("Mike"));
        assertThat((List<Person>) deserialize(serialize(folks))).isEqualTo(folks);
    }

    public static <T> T deserialize(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInput inputStream = new ObjectInputStream(bis)) {
            return (T) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public static byte[] serialize(Object value) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(value);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    static class Person implements Serializable {
        final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
