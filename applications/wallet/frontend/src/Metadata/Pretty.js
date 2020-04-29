import PropTypes from 'prop-types'

import { constants } from '../Styles'

import MetadataPrettyRow from './PrettyRow'

const MetadataPretty = ({ metadata, title, section }) => {
  if (['metrics'].includes(section)) return null

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
  metadata: PropTypes.shape({
    source: PropTypes.shape({
      path: PropTypes.string,
      filename: PropTypes.string,
      extension: PropTypes.string,
      mimetype: PropTypes.string,
    }),
    system: PropTypes.shape({
      projectId: PropTypes.string,
      dataSourceId: PropTypes.string,
      jobId: PropTypes.string,
      taskId: PropTypes.string,
      timeCreated: PropTypes.string,
      state: PropTypes.string,
    }),
    files: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.string,
        size: PropTypes.number,
        name: PropTypes.string,
        mimetype: PropTypes.string,
        category: PropTypes.oneOf(['proxy', 'source', 'web-proxy']),
        attrs: PropTypes.shape({
          width: PropTypes.number,
          height: PropTypes.number,
        }),
      }),
    ),
  }).isRequired,
  title: PropTypes.string.isRequired,
  section: PropTypes.string.isRequired,
}

export default MetadataPretty
