import PropTypes from 'prop-types'

import MetadataAnalysis from './Analysis'
import MetadataPrettyRow from './PrettyRow'
import { constants } from '../Styles'

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
        }}
      >
        <div
          css={{
            borderTop: index !== 0 ? constants.borders.prettyMetadata : '',
          }}
        >
          {Object.entries(file).map(([key, value], i) => (
            <MetadataPrettyRow
              key={key}
              name={key}
              value={value}
              title={title}
              index={i}
              indentation={0}
            />
          ))}
        </div>
      </div>
    ))
  }

  return (
    <div
      css={{
        width: '100%',
      }}
    >
      <div>
        {Object.entries(metadata[section]).map(([key, value], index) => {
          return (
            <MetadataPrettyRow
              key={key}
              name={key}
              value={value}
              title={title}
              index={index}
              indentation={0}
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
