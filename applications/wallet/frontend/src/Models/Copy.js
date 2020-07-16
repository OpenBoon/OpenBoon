import PropTypes from 'prop-types'
import Link from 'next/link'

import { constants, colors } from '../Styles'

const ModelsCopy = ({ projectId }) => {
  return (
    <p
      css={{
        maxWidth: constants.paragraph.maxWidth,
        color: colors.structure.zinc,
      }}
    >
      FINAL COPY NEEDED Steps in adding labels and then coming back to the model
      detail page to train. Once a model has been trained it will be available
      to add to{' '}
      <Link
        href="/[projectId]/data-sources"
        as={`/${projectId}/data-sources`}
        passHref
      >
        <a css={{ color: colors.key.one }}>data sources</a>
      </Link>
      . Learn more about{' '}
      <a
        css={{ color: colors.key.one }}
        target="_blank"
        rel="noopener noreferrer"
        href="https://app.gitbook.com/@zorroa/s/zmlp/client/data-sets"
      >
        custom models
      </a>
      .
    </p>
  )
}

ModelsCopy.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ModelsCopy
