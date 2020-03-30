import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import Accordion from '../Accordion'
import CheckboxTable from '../Checkbox/Table'

const IMG_HEIGHT = 32

const DataSourcesAddModules = ({
  provider: { name, logo, description, categories },
  onClick,
}) => {
  return (
    <Accordion title={<img src={logo} alt={name} height={IMG_HEIGHT} />}>
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
        {categories.map((c) => (
          <CheckboxTable
            key={c.name}
            category={{
              name: c.name,
              options: c.modules.map((m) => ({
                value: m.name,
                label: m.description,
                initialValue: false,
                isDisabled: m.restricted,
              })),
            }}
            onClick={onClick}
          />
        ))}
      </>
    </Accordion>
  )
}

DataSourcesAddModules.propTypes = {
  provider: PropTypes.shape({
    name: PropTypes.string.isRequired,
    logo: PropTypes.node.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        modules: PropTypes.arrayOf(PropTypes.shape()).isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddModules
