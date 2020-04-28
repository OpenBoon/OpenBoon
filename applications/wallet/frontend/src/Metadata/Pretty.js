import PropTypes from 'prop-types'

import { constants } from '../Styles'

import MetadataAnalysis from './Analysis'
import MetadataPrettyRow from './PrettyRow'

const MetadataPretty = ({ metadata, title, section }) => {
  if (['metrics'].includes(section)) return null

  if (section === 'analysis') {
    return <MetadataAnalysis />
  }

  if (Array.isArray(metadata[section])) {
    return metadata[section].map((file, index) => (
      <div
        // eslint-disable-next-line react/no-array-index-key
        key={`${section}${index}`}
        css={{
          width: '100%',
          '&:not(:first-of-type)': {
            borderTop: constants.borders.prettyMetadata,
          },
        }}
      >
        {Object.entries(file).map(([key, value]) => (
          <MetadataPrettyRow key={key} name={key} value={value} path={title} />
        ))}
      </div>
    ))
  }

  return (
    <div css={{ width: '100%' }}>
      <div>
        {Object.entries(metadata[section]).map(([key, value]) => {
          return (
            <MetadataPrettyRow
              key={key}
              name={key}
              value={value}
              path={title}
            />
          )
        })}
      </div>
    </div>
  )
}

MetadataPretty.propTypes = {
  metadata: PropTypes.shape({}).isRequired,
  title: PropTypes.string.isRequired,
  section: PropTypes.string.isRequired,
}

export default MetadataPretty
