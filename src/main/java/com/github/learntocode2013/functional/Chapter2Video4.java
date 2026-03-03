package com.github.learntocode2013.functional;

public class Chapter2Video4 {
    protected record Person(String name, int age) {}
    protected static class DataLoader {
        private final NoArgFunction<Person> personLoader;

        public DataLoader(boolean isDev) {
            this.personLoader = isDev ? this::loadPersonFake : this::loadPersonReal;
        }

        private Person loadPersonReal() {
            System.out.printf("Loading real person %n");
            return new Person("Real person", 38);
        }

        private Person loadPersonFake() {
            System.out.printf("Loading fake person %n");
            return new Person("Fake person", 38);
        }
    }

    public static void main(String[] args) {
        final boolean IS_DEVELOPMENT = false;
        var dataLoader = new DataLoader(IS_DEVELOPMENT);
        dataLoader.personLoader.apply();
    }
}
