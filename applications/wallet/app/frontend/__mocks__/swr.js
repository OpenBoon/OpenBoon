import { createElement } from 'react'

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
  mockUseSWRResponse = data
}

const useSWR = () => {
  return mockUseSWRResponse
}

export default useSWR
