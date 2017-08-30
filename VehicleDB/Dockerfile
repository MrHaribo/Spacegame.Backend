FROM postgres

# Change Password as soon as possible
ENV POSTGRES_PASSWORD=VehicleDB_password_1234

EXPOSE 5432

RUN mkdir ~/VehicleDB_data
VOLUME ~/VehicleDB_data:/var/lib/postgresql/data
COPY init_db.sql /docker-entrypoint-initdb.d/init_db.sql