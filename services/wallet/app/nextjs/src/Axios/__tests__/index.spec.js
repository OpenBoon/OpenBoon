import {
  refreshAuthTokens,
  axiosIntercept,
  decorateHeaders,
  errorHandler,
  axiosCreate,
  fetcher,
} from '../helpers'

describe('<Axios /> helpers', () => {
  describe('refreshAuthTokens()', () => {
    it('should refresh the tokens', () => {
      const mockSetItem = jest.fn()
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: {
          getItem: key => key,
          setItem: mockSetItem,
        },
      })

      const axiosInstance = {
        post: () => ({
          then: fn => fn({ data: { access: 'ACCESS_TOKEN' } }),
        }),
      }

      const failedRequest = { response: { config: { headers: {} } } }

      refreshAuthTokens({ axiosInstance })(failedRequest)

      expect(failedRequest).toMatchSnapshot()

      expect(mockSetItem).toHaveBeenCalledWith('ACCESS_TOKEN', 'ACCESS_TOKEN')
    })
  })

  describe('decorateHeaders()', () => {
    it('should return a function', () => {
      expect(axiosIntercept({})).toMatchSnapshot()
    })
  })

  describe('decorateHeaders()', () => {
    it('should return an object with accessToken', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: { getItem: key => key },
      })

      expect(
        decorateHeaders({ headers: { 'content-type': 'application/json' } }),
      ).toMatchSnapshot()
    })

    it('should return an object without accessToken', () => {
      Object.defineProperty(window, 'localStorage', {
        writable: true,
        value: { getItem: () => '' },
      })

      expect(
        decorateHeaders({ headers: { 'content-type': 'application/json' } }),
      ).toMatchSnapshot()
    })
  })

  describe('errorHandler()', () => {
    it('should return a rejected promise', () => {
      errorHandler('foo').catch(error => expect(error).toEqual('foo'))
    })
  })

  describe('axiosCreate()', () => {
    it('should return a function', () => {
      expect(axiosCreate()).toMatchSnapshot()
    })
  })

  describe('fetcher()', () => {
    it('should return the content of data', () => {
      expect(
        fetcher({
          axiosInstance: () => ({
            then: successCallback => successCallback({ data: 'results' }),
          }),
        })(),
      ).toEqual('results')
    })
  })
})
