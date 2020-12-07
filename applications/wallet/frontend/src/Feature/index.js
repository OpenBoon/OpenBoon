import PropTypes from 'prop-types'
import getConfig from 'next/config'

import { useLocalStorage } from '../LocalStorage/helpers'

export const ENVS = {
  LOCAL: 'localdev',
  DEV: 'zvi-dev',
  QA: 'zvi-qa',
  PROD: 'zvi-prod',
}

const Feature = ({ flag, envs, children }) => {
  const {
    publicRuntimeConfig: { ENVIRONMENT },
  } = getConfig()

  const [featureFlags] = useLocalStorage({
    key: 'FeatureFlags',
    initialState: {},
  })

  // Disabled flag
  if (featureFlags[flag] === false) {
    return null
  }

  // Enabled flag
  if (featureFlags[flag] === true) {
    return children
  }

  // Enabled environments
  if (envs.includes(ENVIRONMENT)) {
    return children
  }

  // Enabled environments by default
  if ([ENVS.LOCAL, ENVS.DEV].includes(ENVIRONMENT)) {
    return children
  }

  return null
}

Feature.propTypes = {
  flag: PropTypes.string.isRequired,
  envs: PropTypes.arrayOf(PropTypes.oneOf(Object.values(ENVS))).isRequired,
}

export default Feature
