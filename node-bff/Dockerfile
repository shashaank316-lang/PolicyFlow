FROM node:24-alpine

ENV NODE_ENV=production

WORKDIR /app

COPY --chown=node:node package.json package-lock.json ./

RUN npm ci --omit=dev \
    && npm cache clean --force

COPY --chown=node:node index.js ./index.js

USER node

EXPOSE 3000

CMD ["node", "index.js"]
