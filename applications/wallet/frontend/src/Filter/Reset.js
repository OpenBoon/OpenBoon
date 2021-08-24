import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from './shape'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from '../Filters/helpers'

import { formatOptions, getValues } from './helpers'

const FilterReset = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filterIndex,
  onReset,
}) => {
  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const options =
    filter.attribute.split('.').reduce((acc, cur) => acc && acc[cur], fields) ||
    []

  if (filter.type === 'exists' && options.includes('similarity')) {
    return null
  }

  return (
    <div
      css={{
        display: 'flex',
        paddingBottom: spacing.base,
      }}
    >
      {options.length > 1 && (
        <select
          defaultValue={filter.type}
          onChange={({ target: { value } }) => {
            const values = value === 'exists' ? { exists: true } : {}

            onReset()

            dispatch({
              type: ACTIONS.UPDATE_FILTER,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                updatedFilter: { ...filter, type: value, values },
                filterIndex,
              },
            })
          }}
          css={{
            padding: spacing.small,
            paddingLeft: spacing.moderate / 2,
            paddingRight: constants.icons.regular + spacing.moderate / 2,
            lineHeight: typography.height.regular,
            fontSize: typography.size.regular,
            fontFamily: typography.family.condensed,
            textTransform: 'uppercase',
            color: colors.structure.zinc,
            border: 'none',
            borderRadius: spacing.mini,
            MozAppearance: 'none',
            WebkitAppearance: 'none',
            backgroundColor: colors.structure.smoke,
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2IzYjNiMyIgLz4KPC9zdmc+')`,
            backgroundRepeat: `no-repeat`,
            backgroundSize: constants.icons.regular,
            backgroundPosition: `right ${spacing.moderate / 4}px top ${
              spacing.moderate / 4
            }px`,
            cursor: 'pointer',
            ':hover, :focus': {
              color: colors.structure.white,
              backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
            },
          }}
        >
          {options.map((option) => {
            return (
              <option key={option} value={option}>
                {formatOptions({ option })}
              </option>
            )
          })}
        </select>
      )}

      <div css={{ flex: 1 }} />

      {filter.type !== 'exists' && (
        <Button
          variant={VARIANTS.MICRO}
          onClick={() => {
            const { type, values: { ids } = {} } = filter

            const values = getValues({ type, ids })

            onReset()

            dispatch({
              type: ACTIONS.UPDATE_FILTER,
              payload: {
                pathname,
                projectId,
                assetId,
                filters,
                updatedFilter: { ...filter, values },
                filterIndex,
              },
            })
          }}
        >
          Reset
        </Button>
      )}
    </div>
  )
}

FilterReset.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
  onReset: PropTypes.func.isRequired,
}

export default FilterReset
