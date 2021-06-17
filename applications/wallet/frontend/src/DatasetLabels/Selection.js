import PropTypes from 'prop-types'
import Router from 'next/router'

import { spacing } from '../Styles'

import { encode } from '../Filters/helpers'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'

const SCOPE_WIDTH = 150

const DatasetLabelsSelection = ({
  projectId,
  datasetId,
  scope,
  label,
  labels,
}) => {
  return (
    <div css={{ display: 'flex' }}>
      <Select
        label="Scope"
        useAria
        options={SCOPE_OPTIONS}
        defaultValue={scope}
        onChange={({ value }) => {
          Router.push(
            `/${projectId}/datasets/${datasetId}/labels?query=${encode({
              filters: { scope: value, label },
            })}`,
          )
        }}
        isRequired={false}
        variant={SELECT_VARIANTS.ROW}
        style={{
          width: SCOPE_WIDTH,
        }}
      />

      <div css={{ width: spacing.normal }} />

      <Select
        label="Label"
        useAria
        options={labels.reduce(
          (acc, { label: l }) => [...acc, { label: l, value: l }],
          [],
        )}
        defaultValue={label}
        onChange={({ value }) => {
          Router.push(
            `/${projectId}/datasets/${datasetId}/labels?query=${encode({
              filters: { scope, label: value },
            })}`,
          )
        }}
        isRequired={false}
        variant={SELECT_VARIANTS.ROW}
      />
    </div>
  )
}

DatasetLabelsSelection.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  scope: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default DatasetLabelsSelection
