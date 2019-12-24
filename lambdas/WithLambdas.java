package lambdas;

import lambdas.Person;

public class WithLambdas {

    public static void main(String[] args) {

        java.util.List<Person> people = java.util.Arrays.asList(
                new lambdas.Person ("Miguel", "Cactics", 39),
                new lambdas.Person ("Pierre", "Cert", 39),
                new lambdas.Person ("Fer", "Zio", 39),
                new lambdas.Person ("Rafa", "XioLaga", 39)
        );


        // ----------------
        //WITH LAMBDAS
        // ----------------

        // Sort list by last name (using lambdas)
        java.util.Collections.sort(people, (p1, p2) -> p1.getLastname().compareTo(p2.getLastname()));

        // Print all elements in the list
        printConditionally(people, p -> true); // Lambda always true, so it will always be printed

        // Print last name beginning with X using a Comparator
        printConditionally(people, p -> p.getLastname().startsWith("X"));
    }

    private static void printConditionally(java.util.List<Person> people, Condition condition){
        for (Person p : people) {
            if (condition.test(p)){
                System.out.println(p);
            }
        }

    }
}
