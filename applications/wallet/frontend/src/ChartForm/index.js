import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors } from '../Styles'

import chartShape from '../Chart/shape'

import ChartFormOptions from './Options'

import { formatFields, getCountList } from './helpers'

const MAX_COUNT = 10

const ChartForm = ({ chart: { type } }) => {
  const {
    query: { projectId },
  } = useRouter()

  const [attribute, setAttribute] = useState('')
  const [count, setCount] = useState(5)

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filteredFields = formatFields({ fields, type })
  const countList = getCountList({ count: MAX_COUNT })

  return (
    <div
      css={{
        width: 450,
        backgroundColor: colors.structure.lead,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <h2>{type} Visualization</h2>

      <label htmlFor="metadata-type-selection">Metadata Type</label>
      <select
        id="metadata-type-selection"
        defaultValue={attribute}
        onChange={({ target: { value } }) => {
          setAttribute(value)
        }}
      >
        <ChartFormOptions fields={filteredFields} type={type} />
      </select>

      <label htmlFor="visualization-count-selection">
        Select Number of Values Shown (top {MAX_COUNT} max)
      </label>
      <select
        id="visualization-count-selection"
        defaultValue={count}
        onChange={({ target: { value } }) => {
          setCount(value)
        }}
        css={{ width: '50%' }}
      >
        {countList.map((value) => (
          <option>{value}</option>
        ))}
      </select>
    </div>
  )
}

ChartForm.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartForm
