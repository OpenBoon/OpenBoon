import { constants, colors } from '../Styles'

const ApiKeysCopy = () => {
  return (
    <p
      css={{
        maxWidth: constants.paragraph.maxWidth,
        color: colors.structure.zinc,
      }}
    >
      Create and manage API Keys for applications or tools you would like to
      integrate with Boon AI. An API Key is required to access data for this
      project using the Boon AI SDK and REST APIs. For more information please
      visit{' '}
      <a
        css={{ color: colors.key.two }}
        target="_blank"
        rel="noopener noreferrer"
        href="https://docs.boonai.app/boonsdk/working-with-api-keys"
      >
        our documentation
      </a>
      .
    </p>
  )
}

export default ApiKeysCopy
