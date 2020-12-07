import PropTypes from 'prop-types'
import useSWR from 'swr'
import Router, { useRouter } from 'next/router'

import { spacing } from '../Styles'

import { getQueryString } from '../Fetch/helpers'
import { decode, encode } from '../Filters/helpers'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

import Select from '../Select'

const ModelAssetsDropdown = ({ projectId, modelId }) => {
  const {
    query: { query: q },
  } = useRouter()

  const { scope, label } = decode({ query: q })

  const {
    data: { results },
  } = useSWR(`/api/v1/projects/${projectId}/models/${modelId}/get_labels/`)

  const labels = results.reduce(
    (acc, { label: l }) => [...acc, { label: l, value: l }],
    [],
  )

  return (
    <div css={{ display: 'flex' }}>
      <Select
        label="Scope"
        useAria
        options={SCOPE_OPTIONS}
        defaultValue={scope || SCOPE_OPTIONS[0].label}
        onChange={({ value }) => {
          const query = encode({ filters: { scope: value, label } })

          Router.push(
            `/[projectId]/models/[modelId]/assets${getQueryString({ query })}`,
            `/${projectId}/models/${modelId}/assets${getQueryString({
              query,
            })}`,
          )
        }}
        isRequired={false}
        style={{ width: '100%' }}
      />

      <div css={{ width: spacing.normal }} />

      <Select
        label="Label"
        useAria
        options={labels}
        defaultValue={label || labels[0].label}
        onChange={({ value }) => {
          const query = encode({ filters: { scope, label: value } })

          Router.push(
            `/[projectId]/models/[modelId]/assets${getQueryString({ query })}`,
            `/${projectId}/models/${modelId}/assets${getQueryString({
              query,
            })}`,
          )
        }}
        isRequired={false}
        style={{ width: '100%' }}
      />
    </div>
  )
}

ModelAssetsDropdown.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
}

export default ModelAssetsDropdown
