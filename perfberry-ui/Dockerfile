FROM node:16.18.0-alpine3.16

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY package.json /usr/src/app/
COPY package-lock.json /usr/src/app/
RUN npm install --ignore-scripts
COPY . /usr/src/app
RUN npm run postinstall

EXPOSE 3000

CMD [ "npm", "start" ]
