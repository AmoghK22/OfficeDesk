import { useState, useEffect } from 'react'

export default function SlaCountdown({ slaDeadline, slaHours, status }) {
  const [timeLeft, setTimeLeft] = useState('')

  useEffect(() => {
    if (!slaDeadline || status === 'CLOSED' || status === 'RESOLVED') {
      setTimeLeft('')
      return
    }

    const update = () => {
      const now = new Date()
      const deadline = new Date(slaDeadline)
      const diff = deadline - now

      if (diff <= 0) {
        setTimeLeft('EXPIRED')
        return
      }

      const hours = Math.floor(diff / (1000 * 60 * 60))
      const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

      if (hours > 24) {
        const days = Math.floor(hours / 24)
        setTimeLeft(`${days}d ${hours % 24}h`)
      } else if (hours > 0) {
        setTimeLeft(`${hours}h ${mins}m`)
      } else {
        setTimeLeft(`${mins}m`)
      }
    }

    update()
    const interval = setInterval(update, 60000)
    return () => clearInterval(interval)
  }, [slaDeadline, status])

  if (!timeLeft) return null

  const totalMs = new Date(slaDeadline) - new Date()
  const isExpired = totalMs <= 0
  const totalSlaMs = (slaHours || 24) * 60 * 60 * 1000
  const ratio = isExpired ? 0 : totalMs / totalSlaMs

  let colorClass = 'text-green-600'
  if (isExpired) colorClass = 'text-red-600 font-bold'
  else if (ratio < 0.1) colorClass = 'text-red-600'
  else if (ratio < 0.3) colorClass = 'text-orange-500'
  else if (ratio < 0.5) colorClass = 'text-yellow-600'

  return (
    <span className={`text-xs font-medium ${colorClass}`}>
      {isExpired ? '!' : ''}{timeLeft}
    </span>
  )
}
