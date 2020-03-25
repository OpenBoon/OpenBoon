import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const DataSourcesAddModules = ({
  module: { logo, description, categories },
  onClick,
}) => {
  return (
    <Accordion title={logo}>
      <>
        <p
          css={{
            color: colors.structure.zinc,
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
            maxWidth: constants.paragraph.maxWidth,
          }}
        >
          {description}
        </p>
        {Object.entries(categories).map(([name, options]) => (
          <CheckboxTable
            key={name}
            category={{ name, options }}
            onClick={onClick}
          />
        ))}
      </>
    </Accordion>
  )
}

DataSourcesAddModules.propTypes = {
  module: PropTypes.shape({
    logo: PropTypes.node.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.object.isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddModules
