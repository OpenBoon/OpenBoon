import PropTypes from 'prop-types'

import { colors, spacing, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'
import CheckboxPartialSvg from '../Icons/checkboxpartial.svg'
import CrossSvg from '../Icons/cross.svg'

import Accordion, { CHECKMARK_WIDTH } from '../Accordion'
import CheckboxIcon, { VARIANTS } from '../Checkbox/Icon'
import CheckboxTable from '../Checkbox/Table'

const DataSourcesAddModules = ({
  module: { provider, description, categories },
  selectedModules,
  providerModules,
  onClick,
}) => {
  const providerSelectionCount = providerModules.filter(module =>
    selectedModules.includes(module),
  ).length

  const isAllSelected = providerSelectionCount === providerModules.length
  const isPartialSelected = !isAllSelected && providerSelectionCount > 0

  return (
    <Accordion
      title={
        <>
          <label
            css={{
              display: 'flex',
              alignItems: 'center',
              color: colors.white,
              cursor: 'pointer',
              marginRight: spacing.normal,
            }}>
            <div css={{ marginRight: spacing.normal }}>
              <CheckboxIcon
                variant={VARIANTS.SECONDARY}
                value=""
                isChecked={isAllSelected}
                isPartial={isPartialSelected}
                onClick={() => {
                  console.log('clicked')
                }}
              />
            </div>
            {provider}
          </label>
        </>
      }>
      <>
        <p
          css={{
            color: colors.structure.zinc,
            margin: 0,
            paddingTop: spacing.base,
            paddingBottom: spacing.normal,
            maxWidth: constants.paragraph.maxWidth,
          }}>
          {description}
        </p>
        {categories.map(category => (
          <CheckboxTable
            key={category.name}
            variant={VARIANTS.PRIMARY}
            category={category}
            onClick={onClick}
          />
        ))}
      </>
    </Accordion>
  )
}

DataSourcesAddModules.propTypes = {
  module: PropTypes.shape({
    provider: PropTypes.string.isRequired,
    description: PropTypes.node.isRequired,
    categories: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        modules: PropTypes.arrayOf(
          PropTypes.shape({
            key: PropTypes.string.isRequired,
            label: PropTypes.string.isRequired,
            legend: PropTypes.string.isRequired,
          }).isRequired,
        ).isRequired,
      }).isRequired,
    ).isRequired,
  }).isRequired,
  selectedModules: PropTypes.arrayOf(PropTypes.string).isRequired,
  providerModules: PropTypes.arrayOf(PropTypes.string).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default DataSourcesAddModules
