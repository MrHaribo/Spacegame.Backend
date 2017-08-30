FROM postgres

# Change Password as soon as possible
ENV POSTGRES_PASSWORD=RegionDB_password_1234

EXPOSE 5432

RUN mkdir ~/RegionDB_data
VOLUME ~/RegionDB_data:/var/lib/postgresql/data
COPY init_db.sql /docker-entrypoint-initdb.d/init_db.sql