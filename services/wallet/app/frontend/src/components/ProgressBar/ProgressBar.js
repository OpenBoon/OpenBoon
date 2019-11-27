import React from 'react'
import PropTypes from 'prop-types'

function ProgressBar({ status }) {
  const { succeeded, failed, running, pending } = status

  const total = Object.values(status).reduce((current, sum) => { return sum += current })
  const percentSucceeded = Math.ceil(succeeded / total * 100)
  const percentFailed = Math.ceil(failed / total * 100)
  const percentRunning = Math.ceil(running / total * 100)
  const percentPending = Math.ceil(pending / total * 100)

  return (
    <div className="ProgressBar">
      <div className="ProgressBar__Succeeded" style={{ height: '100%', width: `${percentSucceeded}%` }}></div>
      <div className="ProgressBar__Failed" style={{ height: '100%', width: `${percentFailed}%` }}></div>
      <div className="ProgressBar__Running" style={{ height: '100%', width: `${percentRunning}%` }}></div>
      <div className="ProgressBar__Pending" style={{ height: '100%', width: `${percentPending}%` }}></div>
    </div>
  )
}

ProgressBar.propTypes = {
  status: PropTypes.shape({
    succeeded: PropTypes.number,
    failed: PropTypes.number,
    running: PropTypes.number,
    pending: PropTypes.number,
  })
}

export default ProgressBar