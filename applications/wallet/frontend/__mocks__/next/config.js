import dotenv from 'dotenv'

dotenv.config()

const getConfig = () => ({
  serverRuntimeConfig: process.env,
  publicRuntimeConfig: { ...process.env, ENVIRONMENT: 'localdev' },
})

export default getConfig
