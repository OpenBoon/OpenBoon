export const noop = () => {}

export const meFetcher =
  typeof window === 'undefined'
    ? noop
    : async (url) => {
        try {
          const response = await fetch(url)

          if (response.status >= 400) throw response

          const user = await response.json()
          const projectId = user.roles ? Object.keys(user.roles)[0] || '' : ''

          return { ...user, projectId }
        } catch (error) {
          return {}
        }
      }
