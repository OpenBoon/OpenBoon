/* istanbul ignore file */
export const openContactForm = ({
  user: { username, firstName, lastName, email },
}) => {
  if (typeof zE === 'undefined') return

  zE('webWidget', 'identify', {
    name: username,
    email,
  })

  zE('webWidget', 'prefill', {
    name: {
      value: `${firstName} ${lastName}`,
      readOnly: true,
    },
    email: {
      value: email,
      readOnly: true,
    },
  })

  zE('webWidget', 'updateSettings', {
    webWidget: {
      contactForm: {
        suppress: false,
      },
    },
  })

  zE('webWidget', 'show')
  zE('webWidget', 'open')

  zE('webWidget:on', 'close', () => {
    zE('webWidget', 'hide')
  })
}
