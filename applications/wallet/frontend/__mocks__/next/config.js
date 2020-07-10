import dotenv from 'dotenv'

dotenv.config()

let mockPublicRuntimeConfig = { ...process.env, ENVIRONMENT: 'localdev' }

export const __setPublicRuntimeConfig = (data) => {
  mockPublicRuntimeConfig = data
}

const getConfig = () => ({
  serverRuntimeConfig: process.env,
  publicRuntimeConfig: mockPublicRuntimeConfig,
})

export default getConfig
