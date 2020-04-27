import PropTypes from 'prop-types'

import MetadataAnalysis from './Analysis'
import MetadataPrettyRow from './PrettyRow'
import { constants } from '../Styles'

const MetadataPretty = ({ metadata, title, section }) => {
  if (['metrics', 'analysis', 'location'].includes(section)) return null

  if (['files', 'metrics', 'location'].includes(section)) return null

  if (section === 'analysis') {
    return <MetadataAnalysis />
  }
  if (Array.isArray(metadata[section])) {
    return metadata[section].map((file, index) => (
      <table
        css={{
          border: `1px solid white`,
          'td, th': {
            border: `1px solid white`,
          },
          borderCollapse: 'collapse',
          width: '100%',
        }}
      >
        <tbody
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
        </tbody>
      </table>
    ))
  }

  return (
    <table
      css={{
        border: `1px solid white`,
        'td, th': {
          border: `1px solid white`,
        },
        borderCollapse: 'collapse',
        width: '100%',
      }}
    >
      <tbody>
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
      </tbody>
    </table>
  )
}

MetadataPretty.propTypes = {
  metadata: PropTypes.shape({}).isRequired,
  title: PropTypes.string.isRequired,
  section: PropTypes.string.isRequired,
}

export default MetadataPretty
