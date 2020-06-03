import PropTypes from 'prop-types'
import useSWR from 'swr'

import filterShape from './shape'

import { colors, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from '../Filters/helpers'

import { formatOptions } from './helpers'

const FilterReset = ({
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
              action: ACTIONS.UPDATE_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                updatedFilter: { ...filter, type: value, values },
                filterIndex,
              },
            })
          }}
          css={{
            backgroundColor: colors.transparent,
            border: 'none',
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            MozAppearance: 'none',
            WebkitAppearance: 'none',
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2IzYjNiMyIgLz4KPC9zdmc+')`,
            backgroundRepeat: `no-repeat, repeat`,
            backgroundPosition: `right top 50%`,
            paddingRight: spacing.comfy,
            color: colors.structure.zinc,
            fontFamily: 'Roboto Condensed',
            textTransform: 'uppercase',
            cursor: 'pointer',
            ':hover': {
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
          style={{
            width: '100%',
            color: colors.structure.zinc,
            fontFamily: 'Roboto Condensed',
            textTransform: 'uppercase',
            ':hover': {
              color: colors.structure.white,
            },
          }}
          variant={VARIANTS.NEUTRAL}
          onClick={() => {
            const { values: { ids } = {} } = filter

            const values = ids ? { ids } : {}

            onReset()

            dispatch({
              action: ACTIONS.UPDATE_FILTER,
              payload: {
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
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
  onReset: PropTypes.func.isRequired,
}

export default FilterReset
