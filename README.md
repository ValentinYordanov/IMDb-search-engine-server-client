# IMDb-search-engine

## Client and server sides

The server connects to the [IMDb Api](https://www.omdbapi.com/), downloads and saves information from there in json format on local server machine
The client connects to the server and enter commands such as:

- get-movie \<name\> --fields=[field_1, field_2] - prints these fields for the movie, if blank - prints all the information
- get-tv-series \<name\> --season=value - prints the names of the episodes in \<value\> season
- get-movie-poster \<name\> - downloads the poster of the movie to the server machine and transfers it to your computer
- get-movies --order=[asc|desc] --genre=[genre_1] --actors=[actor_1,actor_2] - prints movie names from the server machine ordered by IMDb   Rating, order and genre not required
