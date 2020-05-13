import PropTypes from 'prop-types'

import { constants } from '../Styles'

import MetadataPrettyRow from './Row'

const MetadataPrettyArray = ({ section, path }) => {
  return section.map((attribute, index) => (
    <div
      // eslint-disable-next-line react/no-array-index-key
      key={`${path}${index}`}
      css={{
        width: '100%',
        '&:not(:first-of-type)': {
          borderTop: constants.borders.prettyMetadata,
        },
      }}
    >
      {Object.entries(attribute).map(([key, value]) => (
        <MetadataPrettyRow key={key} name={key} value={value} path={path} />
      ))}
    </div>
  ))
}

MetadataPrettyArray.propTypes = {
  section: PropTypes.arrayOf(PropTypes.shape()).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyArray
