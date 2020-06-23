import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useRouter } from 'next/router'

import { colors } from '../Styles'

import chartShape from '../Chart/shape'

import ChartFormOptions from './Options'

import { formatFields } from './helpers'

const ChartForm = ({ chart: { type } }) => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const filteredFields = formatFields({ fields, type })

  return (
    <div css={{ width: 450, backgroundColor: colors.structure.lead }}>
      <h2>{type} Visualization</h2>
      <select>
        <ChartFormOptions fields={filteredFields} type={type} />
      </select>
    </div>
  )
}

ChartForm.propTypes = {
  chart: PropTypes.shape(chartShape).isRequired,
}

export default ChartForm
