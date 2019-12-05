FROM nginx
ENV DOLLAR $
COPY default.conf.template /etc/nginx/conf.d/default.conf.template
ENTRYPOINT ["/bin/bash", "-c", "envsubst < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
