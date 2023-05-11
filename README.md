# Perfberry

## Try local

Start containers:
```shell
$ docker-compose up -d
```

OR build and start containers:
```shell
$ docker-compose -f docker-compose.build.yml up --build -d
```

Insert new project:
```shell
$ docker-compose exec postgres psql -U perfberry -c "INSERT INTO projects (alias, name, overview, searches, graphs, apdex, assertions) VALUES ('sandbox', 'Sandbox', '[]', '[]', '[]', '[]', '[]')"

INSERT 0 1
```

Check project list:
```shell
$ docker-compose exec postgres psql -U perfberry -c "SELECT * FROM projects"

 id |  alias  |  name   | overview | searches | graphs | apdex | assertions 
----+---------+---------+----------+----------+--------+-------+------------
  1 | sandbox | Sandbox | []       | []       | []     | []    | []
```

Also from API:
http://localhost:9000/v1/projects
```json
[
    {
        "id": 1,
        "alias": "sandbox",
        "name": "Sandbox"
    }
]
```

Check containers status:
```shell
$ docker-compose ps
        Name                      Command                  State               Ports         
---------------------------------------------------------------------------------------------
perfberry_api_1        /bin/sh -c /app/bin/perfbe ...   Up             0.0.0.0:9000->9000/tcp
perfberry_cli_1        /bin/sh -c perfberry-cli         Exit 0                               
perfberry_postgres_1   docker-entrypoint.sh postgres    Up             0.0.0.0:5432->5432/tcp
perfberry_ui_1         docker-entrypoint.sh npm start   Up             0.0.0.0:3000->3000/tcp
```

Run example:
```shell
$ docker-compose run cli perfberry-cli playbooks run /example/perfberry.yml

Creating perfberry_cli_run ... done
2022/10/25 10:38:58 Reading playbook from /example/perfberry.yml ...
2022/10/25 10:38:58 Initializing playbook ...
2022/10/25 10:38:58 Running playbook ...
2022/10/25 10:38:58 
2022/10/25 10:38:58 Running job 1/1 ...
2022/10/25 10:38:58 Saving report options to report-TMErXztF.yml ...
2022/10/25 10:38:58 Saving build options to build-PgIhITYO.yml ...
2022/10/25 10:38:58 Saving assertions options to assertions-wJnWnUhF.yml ...
2022/10/25 10:38:58 Reading report from report-TMErXztF.yml ...
2022/10/25 10:38:58 Reading build from build-PgIhITYO.yml ...
2022/10/25 10:38:58 Reading assertions from assertions-wJnWnUhF.yml ...
2022/10/25 10:38:58 Searching logs at example ...
2022/10/25 10:38:58 Posting this logs:
2022/10/25 10:38:58 example/phout_0.log
2022/10/25 10:38:59 Report link: http://localhost:3000/projects/sandbox/reports/1
2022/10/25 10:38:59 Checks assertions and report status...
2022/10/25 10:38:59 Status is PASSED.
2022/10/25 10:38:59 Job complete.
2022/10/25 10:38:59 
2022/10/25 10:38:59 Playbook complete.
```

Follow report link from job output or go to UI at: http://localhost:3000

Clean environment:
```shell
$ docker-compose down
```
