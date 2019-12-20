import PropTypes from 'prop-types'

import { typography } from '../Styles'

import NoJobsSvg from '../Icons/noJobs.svg'

const TableEmpty = ({ numColumns }) => {
  return (
    <tr>
      <td colSpan={numColumns}>
        <div
          css={{
            height: '300px',
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexDirection: 'column',
            color: 'red',
            fontSize: typography.size.kilo,
            lineHeight: typography.height.kilo,
          }}>
          <NoJobsSvg width={40} color="red" />
          <div>There are currently no jobs in the queue.</div>
          <div>Any new job will appear here.</div>
        </div>
      </td>
    </tr>
  )
}

TableEmpty.propTypes = {
  numColumns: PropTypes.number.isRequired,
}

export default TableEmpty
