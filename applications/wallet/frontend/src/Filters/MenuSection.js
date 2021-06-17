import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from '../Filter/shape'

import { spacing, typography, constants } from '../Styles'

import FiltersMenuOption from './MenuOption'

const FiltersMenuSection = ({
  projectId,
  path,
  attribute,
  value,
  filters,
  onClick,
}) => {
  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/all/`)

  const { moduleName: label } =
    path === 'labels'
      ? models.find(({ id }) => id === attribute) || {}
      : { moduleName: attribute }

  const fullPath = `${path}.${label}`

  if (!label) return null

  if (Array.isArray(value) && value.length === 0) return null

  if (Array.isArray(value)) {
    return (
      <FiltersMenuOption
        key={fullPath}
        option={fullPath}
        label={label}
        filters={filters}
        onClick={onClick({
          type: value[0],
          attribute: fullPath,
          ...(path === 'labels' ? { datasetId: attribute } : {}),
        })}
      />
    )
  }

  return (
    <div
      key={fullPath}
      css={{
        marginLeft: -spacing.normal,
        marginRight: -spacing.normal,
        padding: spacing.moderate,
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
        borderTop: constants.borders.large.smoke,
        ':first-of-type': {
          paddingTop: 0,
          borderTop: 'none',
        },
        ':last-of-type': {
          paddingBottom: 0,
        },
      }}
    >
      <h4
        css={{
          fontFamily: typography.family.mono,
          fontWeight: typography.weight.regular,
        }}
      >
        {attribute}
      </h4>

      {Object.entries(value).map(([subKey, subValue]) => (
        <FiltersMenuSection
          key={subKey}
          projectId={projectId}
          path={fullPath}
          attribute={subKey}
          value={subValue}
          filters={filters}
          onClick={onClick}
        />
      ))}
    </div>
  )
}

FiltersMenuSection.propTypes = {
  projectId: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  attribute: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.array, PropTypes.object]).isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default FiltersMenuSection
