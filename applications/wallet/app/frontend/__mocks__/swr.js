import { createElement } from 'react'

const noop = () => () => {}

/**
 * <SWRConfig />
 */

export const SWRConfig = ({ children, ...rest }) =>
  createElement('SWRConfig', rest, children)

/**
 * useSWR
 */

let mockUseSWRResponse = {}

export const __setMockUseSWRResponse = data => {
  mockUseSWRResponse = { revalidate: noop, ...data }
}

const useSWR = () => {
  return mockUseSWRResponse
}

export default useSWR
