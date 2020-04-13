import { createElement } from 'react'

const noop = () => () => {}

/**
 * <SWRConfig />
 */

export const SWRConfig = ({ children, ...rest }) =>
  createElement('SWRConfig', rest, children)

/**
 * cache
 */

export const { cache, useSWRPages } = jest.requireActual('swr')

/**
 * useSWR
 */

let mockMutateFn = () => {}

export const __setMockMutateFn = (fn) => {
  mockMutateFn = fn
}

export const mutate = (_, cb) => {
  return mockMutateFn(typeof cb === 'function' ? cb() : cb)
}

/**
 * useSWR
 */

let mockUseSWRResponse = {}

export const __setMockUseSWRResponse = (data) => {
  mockUseSWRResponse = { revalidate: noop, ...data }
}

const useSWR = () => {
  return mockUseSWRResponse
}

export default useSWR
