export const noop = () => {}

export const meFetcher =
  typeof window === 'undefined'
    ? noop
    : async (url) => {
        try {
          const response = await fetch(url)
          if (response.status >= 400) throw response
          return response.json()
        } catch (error) {
          return {}
        }
      }
