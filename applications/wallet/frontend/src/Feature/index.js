import { useEffect } from 'react'
import PropTypes from 'prop-types'
import getConfig from 'next/config'
import { useRouter } from 'next/router'

import { useLocalStorageState } from '../LocalStorage/helpers'

import { parseFeatureFlags } from './helpers'

export const ENVS = {
  LOCAL: 'localdev',
  DEV: 'zvi-dev',
  QA: 'zvi-qa',
  PROD: 'zvi-prod',
}

const {
  publicRuntimeConfig: { ENVIRONMENT },
} = getConfig()

const Feature = ({ flag, envs, children }) => {
  const {
    query: { flags },
  } = useRouter()

  const [featureFlags, setFeatureFlags] = useLocalStorageState({
    key: 'FeatureFlags',
    initialValue: {},
  })

  useEffect(() => {
    if (flags) {
      const parsedFlags = parseFeatureFlags({ flags })

      setFeatureFlags({ value: { ...featureFlags, ...parsedFlags } })
    }
    // Only run this effect on component mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Disabled flag
  if (featureFlags[flag] === false) {
    return null
  }

  // Enabled flag
  if (featureFlags[flag] === true) {
    return children
  }

  // Enabled environment
  if (envs.includes(ENVIRONMENT)) {
    return children
  }

  return null
}

Feature.propTypes = {
  flag: PropTypes.string.isRequired,
  envs: PropTypes.arrayOf(PropTypes.oneOf(Object.values(ENVS))).isRequired,
}

export default Feature
