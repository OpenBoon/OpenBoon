import dotenv from 'dotenv'

dotenv.config()

const getConfig = () => ({
  serverRuntimeConfig: process.env,
  publicRuntimeConfig: process.env,
})

export default getConfig
