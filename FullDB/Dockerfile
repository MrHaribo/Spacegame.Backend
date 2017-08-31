FROM postgres

# Change Password as soon as possible
ENV POSTGRES_PASSWORD=FullDB_password_1234

EXPOSE 5432

RUN mkdir ~/FullDB_data
VOLUME ~/FullDB_data:/var/lib/postgresql/data
COPY init_db.sql /docker-entrypoint-initdb.d/init_db.sql