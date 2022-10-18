import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {

    public List<Movie> movies = new ArrayList<>();

    public MovieAnalyzer(String dataset_path) throws IOException {
        Stream<String> lines = Files.lines(Paths.get(dataset_path));
        lines.forEach(line -> {
            String[] strings = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);;
            if (!Objects.equals(strings[0], "Poster_Link")) {
                String Series_Title = (Objects.equals(strings[1],""))? null : strings[1].replaceAll("\"", "");
                int Released_Year = (Objects.equals(strings[2],""))? -1 : Integer.parseInt(strings[2]);
                String Certificate = (Objects.equals(strings[3],""))? null : strings[3];
                int Runtime = (Objects.equals(strings[4],""))? -1 : Integer.parseInt(strings[4].replaceAll("min","").trim());
                ArrayList<String> Genre = (Objects.equals(strings[5],""))? null :
                        new ArrayList<>(List.of(strings[5].replaceAll("\"", "").replaceAll(" ", "").split(",")));
                float IMDB_Rating = (Objects.equals(strings[6],""))? -1 : Float.parseFloat(strings[6]);
                if(!Objects.equals(strings[7],"")){
                    if(strings[7].startsWith("\"") && strings[7].endsWith("\"")){
                        strings[7] = strings[7].substring(1,strings[7].length()-1);
                    }
                }
                String Overview = (strings[7].equals(""))? null: strings[7];
                Long Meta_score = (Objects.equals(strings[8],""))? null : Long.parseLong(strings[8]);
                String Director = (Objects.equals(strings[9],""))? null : strings[9];
                String Star1 = (Objects.equals(strings[10],""))? null : strings[10];
                String Star2 = (Objects.equals(strings[11],""))? null : strings[11];
                String Star3 = (Objects.equals(strings[12],""))? null : strings[12];
                String Star4 = (Objects.equals(strings[13],""))? null : strings[13];
                Long Noofvotes = (Objects.equals(strings[14],""))? null : Long.parseLong(strings[14]);
                Long Gross = (Objects.equals(strings[15],""))? null : Long.parseLong(strings[15].replaceAll(",","").replaceAll("\"", ""));
                movies.add(new Movie(Series_Title, Released_Year, Certificate, Runtime, Genre, IMDB_Rating, Overview,
                        Meta_score, Director, Star1, Star2, Star3, Star4, Noofvotes, Gross));
            }
        });
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        Map<Integer, Long> countsByYear = this.movies.stream()
                .filter(movie -> movie.Released_Year>0)
                .collect(Collectors.groupingBy(Movie::getReleased_Year, Collectors.counting()));
        Map<Integer, Integer> res = new LinkedHashMap<>();
        countsByYear.entrySet().stream()
                .sorted((o1, o2) -> o2.getKey() - o1.getKey())
                .forEachOrdered(x -> res.put(x.getKey(), x.getValue().intValue()));
        return res;
    }

    public Map<String, Integer> getMovieCountByGenre() {
        ArrayList<String> tmp = new ArrayList<>();
        Stream<String> genreStream = tmp.stream();
        for(Movie movie: movies) {
            genreStream = Stream.concat(genreStream, movie.Genre.stream());
        }
        Map<String, Long> countByGenre = genreStream
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));
        Map<String, Integer> res = new LinkedHashMap<>();
        countByGenre.entrySet().stream()
                .sorted(((o1, o2) -> {
                    if(Objects.equals(o1.getValue(), o2.getValue())) {
                        return o1.getKey().compareTo(o2.getKey());
                    }
                    return (int) (o2.getValue() - o1.getValue());
                }))
                .forEachOrdered(x -> res.put(x.getKey(), x.getValue().intValue()));
        return res;
    }

    public Map<List<String>, Integer> getCoStarCount() {
        Map<List<String>, Integer> res = new HashMap<>();
        List<List<String>> coStars = new ArrayList<>();
        this.movies.forEach(movie -> {
            coStars.add(Arrays.asList(movie.getStar1(), movie.getStar2()));
            coStars.add(Arrays.asList(movie.getStar1(), movie.getStar3()));
            coStars.add(Arrays.asList(movie.getStar1(), movie.getStar4()));
            coStars.add(Arrays.asList(movie.getStar2(), movie.getStar3()));
            coStars.add(Arrays.asList(movie.getStar2(), movie.getStar4()));
            coStars.add(Arrays.asList(movie.getStar3(), movie.getStar4()));
        });
        coStars.stream().collect(Collectors.groupingBy(o -> o.stream().sorted().toList(), Collectors.counting()))
                .entrySet().stream()
                .forEach(listLongEntry -> res.put(listLongEntry.getKey(), listLongEntry.getValue().intValue()));
        return res;
    }

    public List<String> getTopMovies(int top_k, String by) {
        List<String> res = new ArrayList<>();
        if(Objects.equals(by, "runtime")) {
            Stream<Movie> stream = this.movies.stream().filter(movie -> movie.getRuntime() >= 0);
            stream = stream.sorted(((o1, o2) -> {
                if(o1.getRuntime() == o2.getRuntime()) {
                    return o1.getSeries_Title().compareTo(o2.getSeries_Title());
                }else {
                    return o2.getRuntime() - o1.getRuntime();
                }
            })).limit(top_k);
            res = stream.map(Movie::getSeries_Title).toList();
        }else if(Objects.equals(by, "overview")) {
            Stream<Movie> stream = this.movies.stream()
                    .filter(movie -> !Objects.equals(movie.getOverview(), null));
            stream = stream.sorted(((o1, o2) -> {
                if(o1.getOverview().length() == o2.getOverview().length()) {
                    return o1.getSeries_Title().compareTo(o2.getSeries_Title());
                }else {
                    return o2.getOverview().length() - o1.getOverview().length();
                }
            })).limit(top_k);
            res = stream.map(Movie::getSeries_Title).toList();
        }
        return res;
    }

    public List<String> getTopStars(int top_k, String by) {
        List<String> res = new ArrayList<>();
        List<Star> stars = new ArrayList<>();
        if(Objects.equals(by, "rating")) {
            Stream<Movie> stream = this.movies.stream().filter(movie -> movie.getIMDB_Rating()>=0);
            stream.forEach(movie -> {
                if(!Objects.equals(movie.getStar1(), null)) {
                    stars.add(new Star(movie.getStar1(), movie.getIMDB_Rating()));
                }
                if(!Objects.equals(movie.getStar2(), null)) {
                    stars.add(new Star(movie.getStar2(), movie.getIMDB_Rating()));
                }
                if(!Objects.equals(movie.getStar3(), null)) {
                    stars.add(new Star(movie.getStar3(), movie.getIMDB_Rating()));
                }
                if(!Objects.equals(movie.getStar4(), null)) {
                    stars.add(new Star(movie.getStar4(), movie.getIMDB_Rating()));
                }
            });
            res = stars.stream().collect(Collectors
                    .groupingBy(Star::getName, Collectors.averagingDouble(Star::getRating)))
                    .entrySet().stream().sorted(((o1, o2) -> {
                        if(Objects.equals(o1.getValue(), o2.getValue())) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                        return o2.getValue().compareTo(o1.getValue());
                    })).limit(top_k).map(Map.Entry::getKey).toList();
        }else if(Objects.equals(by, "gross")) {
            Stream<Movie> stream = this.movies.stream()
                    .filter(movie -> !Objects.equals(movie.getGross(), null));
            stream.forEach(movie -> {
                if(!Objects.equals(movie.getStar1(), null)) {
                    stars.add(new Star(movie.getStar1(), movie.getGross()));
                }
                if(!Objects.equals(movie.getStar2(), null)) {
                    stars.add(new Star(movie.getStar2(), movie.getGross()));
                }
                if(!Objects.equals(movie.getStar3(), null)) {
                    stars.add(new Star(movie.getStar3(), movie.getGross()));
                }
                if(!Objects.equals(movie.getStar4(), null)) {
                    stars.add(new Star(movie.getStar4(), movie.getGross()));
                }
            });
            res = stars.stream().collect(Collectors
                            .groupingBy(Star::getName, Collectors.averagingLong(Star::getGross)))
                    .entrySet().stream().sorted(((o1, o2) -> {
                        if(Objects.equals(o1.getValue(), o2.getValue())) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                        return o2.getValue().compareTo(o1.getValue());
                    })).limit(top_k).map(Map.Entry::getKey).toList();
        }
        return res;
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        Stream<Movie> stream = this.movies.stream()
                .filter(movie -> !movie.getGenre().isEmpty() &&
                        movie.getIMDB_Rating() >= 0 &&
                        movie.getRuntime() >= 0);
        List<String> res = new ArrayList<>(stream.filter(movie -> movie.getGenre().contains(genre) &&
                movie.getIMDB_Rating() >= min_rating
                && movie.getRuntime() <= max_runtime).map(Movie::getSeries_Title).toList());
        Collections.sort(res);
        return res;
    }

    public static class Movie {
        private String Series_Title;        //Name of the movie
        private int Released_Year;          // Year at which that movie released
        private String Certificate;         //Certificate earned by that movie
        private int Runtime;                //Total runtime of the movie, unit:min
        private ArrayList<String> Genre;    //Genre of the movie
        private float IMDB_Rating;         //Rating of the movie at IMDB site
        private String Overview;            //mini story/ summary
        private Long Meta_score;             //Score earned by the movie
        private String Director;            //Name of the Director
        private String Star1, Star2, Star3, Star4; //Name of the Stars
        private Long Noofvotes;             //Total number of votes
        private Long Gross;                 //Money earned by that movie

        public Movie(String series_Title, int released_Year, String certificate, int runtime, ArrayList<String> genre, float IMDB_Rating, String overview, Long meta_score, String director, String star1, String star2, String star3, String star4, Long noofvotes, Long gross) {
            Series_Title = series_Title;
            Released_Year = released_Year;
            Certificate = certificate;
            Runtime = runtime;
            Genre = genre;
            this.IMDB_Rating = IMDB_Rating;
            Overview = overview;
            Meta_score = meta_score;
            Director = director;
            Star1 = star1;
            Star2 = star2;
            Star3 = star3;
            Star4 = star4;
            Noofvotes = noofvotes;
            Gross = gross;
        }

        public String getSeries_Title() {
            return Series_Title;
        }

        public int getReleased_Year() {
            return Released_Year;
        }

        public String getCertificate() {
            return Certificate;
        }

        public int getRuntime() {
            return Runtime;
        }

        public ArrayList<String> getGenre() {
            return Genre;
        }

        public float getIMDB_Rating() {
            return IMDB_Rating;
        }

        public String getOverview() {
            return Overview;
        }

        public Long getMeta_score() {
            return Meta_score;
        }

        public String getDirector() {
            return Director;
        }

        public String getStar1() {
            return Star1;
        }

        public String getStar2() {
            return Star2;
        }

        public String getStar3() {
            return Star3;
        }

        public String getStar4() {
            return Star4;
        }

        public Long getNoofvotes() {
            return Noofvotes;
        }

        public Long getGross() {
            return Gross;
        }
    }

    public static class Star {
        private String name;
        private double rating;
        private Long gross;

        public Star(String name, double rating) {
            this.name = name;
            this.rating = rating;
        }

        public Star(String name, Long gross) {
            this.name = name;
            this.gross = gross;
        }

        public String getName() {
            return name;
        }

        public double getRating() {
            return rating;
        }

        public Long getGross() {
            return gross;
        }
    }

    public static void main(String[] args) {
    }

}

