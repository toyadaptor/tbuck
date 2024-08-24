FROM node:18-alpine AS build-stage

WORKDIR /app

COPY . .

#RUN npm install

RUN npx shadow-cljs release frontend

FROM nginx:alpine AS production-stage

# Nginx 설정 파일 복사 (필요시 설정)
#COPY nginx.conf /etc/nginx/nginx.conf

COPY --from=build-stage /app/resources/public /usr/share/nginx/html
CMD ["nginx", "-g", "daemon off;"]