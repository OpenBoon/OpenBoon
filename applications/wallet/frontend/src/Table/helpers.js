import Router from 'next/router'

export const onRowClickRouterPush = (...args) => (event) => {
  const { target: { localName } = {} } = event || {}

  if (['a', 'button', 'svg', 'path'].includes(localName)) return

  Router.push(...args)
}
