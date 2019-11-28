export const axiosCreate = () => ({
  post: () => ({
    then: successCallback => {
      successCallback({ data: { access: 'access', refresh: 'refresh' } })
    },
  }),
})
