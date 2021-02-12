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

let mockCacheKeys = []

export const __setMockCacheKeys = (data) => {
  mockCacheKeys = data
}

let mockCacheDeleteFn = () => {}

export const __setMockCacheDeleteFn = (fn) => {
  mockCacheDeleteFn = fn
}

export const cache = {
  keys: () => {
    return mockCacheKeys
  },
  clear: () => {},
  delete: (...args) => {
    mockCacheDeleteFn(...args)
  },
}

/**
 * mutate
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

/**
 * useSWRInfinite
 */

let mockData = []
let mockError

export const __setMockUseSWRInfiniteResponse = ({ data, error }) => {
  mockData = data
  mockError = error
}

export const useSWRInfinite = () => {
  return {
    data: mockData,
    mutate: () => {},
    size: 10,
    setSize: () => {},
    error: mockError,
  }
}
