import PropTypes from 'prop-types'
import useSWR from 'swr'

import { getRelativeUrl } from './helpers'

const FetchAhead = ({ url }) => {
  const relativeUrl = getRelativeUrl({ url })

  useSWR(relativeUrl, {
    suspense: false,
    revalidateOnFocus: false,
    revalidateOnReconnect: false,
    shouldRetryOnError: false,
  })

  return null
}

FetchAhead.propTypes = {
  url: PropTypes.string.isRequired,
}

export default FetchAhead
